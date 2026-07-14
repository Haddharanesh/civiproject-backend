from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile
from sqlalchemy.orm import Session

from app.database import get_db
from app.deps import get_current_user, require_roles
from app.models import Complaint, User
from app.schemas import ComplaintOut, CreateComplaintResponse
from app.services import complaint_service, duplicate_service, vote_service

router = APIRouter(prefix="/api/complaints", tags=["complaints"])


@router.post("", response_model=CreateComplaintResponse)
async def create_complaint(
    description: str = Form(...),
    category: str = Form(...),
    latitude: float = Form(...),
    longitude: float = Form(...),
    image: UploadFile | None = File(None),
    user: User = Depends(require_roles("USER")),
    db: Session = Depends(get_db),
):
    # Add-on: duplicate detection before creating a new complaint
    duplicate = duplicate_service.find_duplicate_complaint(
        db,
        description=description,
        category=category,
        latitude=latitude,
        longitude=longitude,
    )

    if duplicate:
        complaint, vote_added, vote_msg = vote_service.try_upvote(db, user.id, duplicate.id)
        if vote_added:
            message = (
                "A similar complaint already exists nearby. Your support has been added."
            )
        else:
            message = vote_msg  # "You have already supported this complaint."
        return CreateComplaintResponse(
            isDuplicate=True,
            voteAdded=vote_added,
            message=message,
            complaint=ComplaintOut.model_validate(complaint),
        )

    complaint = Complaint(
        description=description,
        category=category,
        latitude=latitude,
        longitude=longitude,
    )
    if image and image.filename:
        complaint.image_url = await complaint_service.save_upload(image)

    created = complaint_service.create_complaint(db, complaint, user.email)
    return CreateComplaintResponse(
        isDuplicate=False,
        voteAdded=False,
        message="Issue reported successfully",
        complaint=ComplaintOut.model_validate(created),
    )


@router.get("/public", response_model=list[ComplaintOut])
def get_public_complaints(
    _: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return complaint_service.get_all_complaints(db)


@router.get("/my", response_model=list[ComplaintOut])
def get_my_complaints(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    return complaint_service.get_complaints_by_user_email(db, user.email)


@router.get("/stats")
def get_stats(_: User = Depends(get_current_user), db: Session = Depends(get_db)):
    return complaint_service.get_stats(db)


@router.get("/filter", response_model=list[ComplaintOut])
def filter_by_status(
    status: str,
    _: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return complaint_service.get_by_status(db, status)


@router.get("/complaints/filter", response_model=list[ComplaintOut])
def get_filtered_complaints(
    status: str,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    if not user.department:
        raise HTTPException(status_code=403, detail="Unauthorized")
    return complaint_service.get_complaints_by_department_and_status(db, user.department, status)


@router.get("/{complaint_id}", response_model=ComplaintOut)
def get_complaint(
    complaint_id: int,
    _: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    try:
        return complaint_service.get_complaint_by_id(db, complaint_id)
    except ValueError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc


@router.get("", response_model=list[ComplaintOut])
def get_all_complaints(_: User = Depends(get_current_user), db: Session = Depends(get_db)):
    return complaint_service.get_all_complaints(db)


@router.put("/{complaint_id}/status", response_model=ComplaintOut)
def update_status(
    complaint_id: int,
    status: str,
    _: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return complaint_service.update_status(db, complaint_id, status)


@router.post("/{complaint_id}/image", response_model=ComplaintOut)
async def upload_image(
    complaint_id: int,
    file: UploadFile = File(...),
    _: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return await complaint_service.upload_image(db, complaint_id, file)
