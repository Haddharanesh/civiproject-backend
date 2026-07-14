from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.database import get_db
from app.deps import require_roles
from app.models import User
from app.schemas import ComplaintOut
from app.services import complaint_service

router = APIRouter(prefix="/api/department", tags=["department"])


def _require_department_admin(user: User) -> User:
    if not user.department:
        raise HTTPException(status_code=403, detail="Department admin not properly assigned")
    return user


@router.get("/complaints", response_model=list[ComplaintOut])
def get_department_complaints(
    user: User = Depends(require_roles("DEPARTMENT_ADMIN")),
    db: Session = Depends(get_db),
):
    _require_department_admin(user)
    return complaint_service.get_complaints_by_department(db, user.department)


@router.get("/complaints/filter", response_model=list[ComplaintOut])
def filter_department_complaints(
    status: str,
    user: User = Depends(require_roles("DEPARTMENT_ADMIN")),
    db: Session = Depends(get_db),
):
    _require_department_admin(user)
    return complaint_service.get_complaints_by_department_and_status(db, user.department, status)


@router.put("/complaints/{complaint_id}/status", response_model=ComplaintOut)
def update_complaint_status(
    complaint_id: int,
    status: str,
    user: User = Depends(require_roles("DEPARTMENT_ADMIN")),
    db: Session = Depends(get_db),
):
    _require_department_admin(user)
    complaint = complaint_service.get_complaint_by_id(db, complaint_id)
    if complaint.department_id != user.department_id:
        raise HTTPException(status_code=403, detail="You cannot update complaints of another department")
    return complaint_service.update_status(db, complaint_id, status)


@router.put("/complaints/{complaint_id}/remarks", response_model=ComplaintOut)
def add_remarks(
    complaint_id: int,
    remarks: str,
    user: User = Depends(require_roles("DEPARTMENT_ADMIN")),
    db: Session = Depends(get_db),
):
    _require_department_admin(user)
    complaint = complaint_service.get_complaint_by_id(db, complaint_id)
    if complaint.department_id != user.department_id:
        raise HTTPException(status_code=403, detail="You cannot add remarks to another department's complaint")
    return complaint_service.add_remarks(db, complaint_id, remarks)


@router.get("/dashboard/stats")
def get_dashboard_stats(
    user: User = Depends(require_roles("DEPARTMENT_ADMIN")),
    db: Session = Depends(get_db),
):
    _require_department_admin(user)
    return complaint_service.get_department_stats(db, user.department)


@router.get("/reports/priority")
def get_priority_breakdown(
    user: User = Depends(require_roles("DEPARTMENT_ADMIN")),
    db: Session = Depends(get_db),
):
    _require_department_admin(user)
    return complaint_service.get_priority_breakdown(db, user.department)
