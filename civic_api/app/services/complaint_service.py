import time
from pathlib import Path

from fastapi import UploadFile
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.config import settings
from app.models import Complaint, Department, User
from app.services import notification_service


def route_department_from_description(description: str | None) -> str | None:
    if not description or not description.strip():
        return None

    desc = description.lower()

    if any(k in desc for k in ("street light", "light", "electric", "power", "transformer", "wire")):
        return "Electricity Board"
    if any(k in desc for k in ("water", "leakage", "pipe", "drainage", "sewage", "tap", "flood")):
        return "Water Supply Department"
    if any(k in desc for k in ("garbage", "waste", "trash", "cleaning", "dirty", "smell")):
        return "Sanitation Department"
    if any(k in desc for k in ("road", "pothole", "street", "bridge", "footpath")):
        return "Public Works Department"
    return None


def route_department_from_category(category: str) -> str:
    category_lower = category.lower()
    mapping = {
        ("road", "pothole", "street"): "Public Works Department",
        ("waste", "garbage", "cleanliness"): "Sanitation Department",
        ("electricity", "streetlight"): "Electricity Board",
        ("water", "drainage", "sewage"): "Water Supply Department",
    }
    for keys, dept in mapping.items():
        if category_lower in keys:
            return dept
    return "General Municipal Department"


def calculate_priority(description: str | None) -> str:
    if not description:
        return "MEDIUM"
    desc_lower = description.lower()
    if any(
        k in desc_lower
        for k in ("fire", "danger", "accident", "emergency", "urgent", "critical", "flood", "collapse")
    ):
        return "HIGH"
    if any(k in desc_lower for k in ("minor", "small", "suggestion", "please")):
        return "LOW"
    return "MEDIUM"


def capitalize_first(text: str) -> str:
    if not text:
        return text
    return text[0].upper() + text[1:]


def generate_title_from_description(description: str | None) -> str:
    if not description or not description.strip():
        return "Untitled Issue"
    if len(description) <= 50:
        return capitalize_first(description.strip())

    words = description.strip().split()
    title_parts: list[str] = []
    for word in words[:8]:
        title_parts.append(word)
        if len(title_parts) >= 6 and len(" ".join(title_parts)) > 45:
            break

    title = capitalize_first(" ".join(title_parts).rstrip(".,;:!? "))
    if len(title) > 50:
        title = title[:50].rstrip(".,;:!? ")
    return title


def _resolve_department(db: Session, description: str | None, category: str) -> Department:
    dept_name = route_department_from_description(description)
    if dept_name:
        department = db.query(Department).filter(Department.name == dept_name).first()
        if department:
            return department

    dept_name = route_department_from_category(category)
    department = db.query(Department).filter(Department.name == dept_name).first()
    if not department:
        raise ValueError(f"Department not found: {dept_name}")
    return department


def create_complaint(db: Session, complaint: Complaint, email: str) -> Complaint:
    user = db.query(User).filter(User.email == email).first()
    if not user:
        raise ValueError("User not found")

    complaint.user = user
    complaint.status = "NEW"
    complaint.department = _resolve_department(db, complaint.description, complaint.category or "")
    complaint.priority = calculate_priority(complaint.description)
    if not complaint.title or not complaint.title.strip():
        complaint.title = generate_title_from_description(complaint.description)

    db.add(complaint)
    db.commit()
    db.refresh(complaint)

    message = (
        f"New complaint: {complaint.title} "
        f"(Category: {complaint.category}, Priority: {complaint.priority})"
    )
    notification_service.create_department_notification(
        db, complaint.department_id, message, "NEW_COMPLAINT", complaint.id
    )
    return complaint


def get_complaint_by_id(db: Session, complaint_id: int) -> Complaint:
    complaint = db.query(Complaint).filter(Complaint.id == complaint_id).first()
    if not complaint:
        raise ValueError("Complaint not found")
    return complaint


def update_status(db: Session, complaint_id: int, status: str) -> Complaint:
    complaint = get_complaint_by_id(db, complaint_id)
    old_status = complaint.status
    complaint.status = status
    db.commit()
    db.refresh(complaint)

    message = (
        f"Your complaint '{complaint.title}' status changed from "
        f"{old_status.replace('_', ' ')} to {status.replace('_', ' ')}"
    )
    notification_service.create_user_notification(
        db, complaint.user_id, message, "STATUS_UPDATE", complaint.id
    )
    return complaint


def add_remarks(db: Session, complaint_id: int, remarks: str) -> Complaint:
    complaint = get_complaint_by_id(db, complaint_id)
    complaint.remarks = remarks
    db.commit()
    db.refresh(complaint)

    preview = remarks if len(remarks) <= 100 else remarks[:100] + "..."
    message = f"Department added a remark to your complaint '{complaint.title}': {preview}"
    notification_service.create_user_notification(db, complaint.user_id, message, "REMARK", complaint.id)
    return complaint


async def save_upload(file: UploadFile) -> str:
    filename = f"{int(time.time() * 1000)}_{file.filename}"
    path = settings.upload_dir / filename
    content = await file.read()
    path.write_bytes(content)
    return f"uploads/{filename}"


async def upload_image(db: Session, complaint_id: int, file: UploadFile) -> Complaint:
    complaint = get_complaint_by_id(db, complaint_id)
    complaint.image_url = await save_upload(file)
    db.commit()
    db.refresh(complaint)
    return complaint


def get_all_complaints(db: Session) -> list[Complaint]:
    return db.query(Complaint).all()


def get_by_status(db: Session, status: str) -> list[Complaint]:
    return db.query(Complaint).filter(Complaint.status == status).all()


def get_stats(db: Session) -> dict[str, int]:
    return {
        "NEW": db.query(Complaint).filter(Complaint.status == "NEW").count(),
        "IN_PROGRESS": db.query(Complaint).filter(Complaint.status == "IN_PROGRESS").count(),
        "RESOLVED": db.query(Complaint).filter(Complaint.status == "RESOLVED").count(),
    }


def get_complaints_by_user_email(db: Session, email: str) -> list[Complaint]:
    user = db.query(User).filter(User.email == email).first()
    if not user:
        raise ValueError("User not found")
    return db.query(Complaint).filter(Complaint.user_id == user.id).all()


def get_complaints_by_department(db: Session, department: Department) -> list[Complaint]:
    return db.query(Complaint).filter(Complaint.department_id == department.id).all()


def get_complaints_by_department_and_status(
    db: Session, department: Department, status: str
) -> list[Complaint]:
    return (
        db.query(Complaint)
        .filter(Complaint.department_id == department.id, Complaint.status == status)
        .all()
    )


def get_department_stats(db: Session, department: Department) -> dict[str, int]:
    return {
        "TOTAL": db.query(Complaint).filter(Complaint.department_id == department.id).count(),
        "NEW": db.query(Complaint)
        .filter(Complaint.department_id == department.id, Complaint.status == "NEW")
        .count(),
        "IN_PROGRESS": db.query(Complaint)
        .filter(Complaint.department_id == department.id, Complaint.status == "IN_PROGRESS")
        .count(),
        "RESOLVED": db.query(Complaint)
        .filter(Complaint.department_id == department.id, Complaint.status == "RESOLVED")
        .count(),
        "HIGH_PRIORITY": db.query(Complaint)
        .filter(Complaint.department_id == department.id, Complaint.priority == "HIGH")
        .count(),
    }


def get_priority_breakdown(db: Session, department: Department) -> dict[str, int]:
    rows = (
        db.query(Complaint.priority, func.count(Complaint.id))
        .filter(Complaint.department_id == department.id)
        .group_by(Complaint.priority)
        .all()
    )
    return {priority: count for priority, count in rows}
