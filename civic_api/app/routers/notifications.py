from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.database import get_db
from app.deps import get_current_user, require_roles
from app.models import User
from app.schemas import ComplaintOut, NotificationOut, VoteResponse
from app.services import notification_service, vote_service

router = APIRouter(tags=["votes", "notifications"])


@router.post("/api/votes", response_model=VoteResponse)
def vote(
    userId: int,
    complaintId: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    # Prefer authenticated user identity for one-vote-per-user integrity
    effective_user_id = current_user.id if current_user.id else userId
    try:
        complaint, vote_added, message = vote_service.try_upvote(db, effective_user_id, complaintId)
        if not vote_added:
            raise HTTPException(status_code=400, detail=message)
        return VoteResponse(
            voteAdded=True,
            message=message,
            complaint=ComplaintOut.model_validate(complaint),
        )
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


notifications_router = APIRouter(prefix="/api/notifications", tags=["notifications"])


@notifications_router.get("/my", response_model=list[NotificationOut])
def get_my_notifications(
    user: User = Depends(require_roles("USER", "DEPARTMENT_ADMIN")),
    db: Session = Depends(get_db),
):
    return notification_service.get_user_notifications(db, user.id)


@notifications_router.get("/department", response_model=list[NotificationOut])
def get_department_notifications(
    user: User = Depends(require_roles("USER", "DEPARTMENT_ADMIN")),
    db: Session = Depends(get_db),
):
    if not user.department:
        raise HTTPException(status_code=400, detail="Department not found")
    return notification_service.get_department_notifications(db, user.department_id)


@notifications_router.get("/unread/count")
def get_unread_count(
    user: User = Depends(require_roles("USER", "DEPARTMENT_ADMIN")),
    db: Session = Depends(get_db),
):
    dept_unread = 0
    if user.department_id:
        dept_unread = notification_service.get_unread_department_count(db, user.department_id)
    return {
        "userUnread": notification_service.get_unread_user_count(db, user.id),
        "departmentUnread": dept_unread,
    }


@notifications_router.get("/{user_id}", response_model=list[NotificationOut])
def get_notifications_by_user_id(
    user_id: int,
    user: User = Depends(require_roles("USER", "DEPARTMENT_ADMIN")),
    db: Session = Depends(get_db),
):
    if user.id != user_id:
        raise HTTPException(status_code=403, detail="Forbidden")
    return notification_service.get_user_notifications(db, user_id)


@notifications_router.put("/{notification_id}/read", status_code=204)
def mark_as_read(
    notification_id: int,
    _: User = Depends(require_roles("USER", "DEPARTMENT_ADMIN")),
    db: Session = Depends(get_db),
):
    notification_service.mark_as_read(db, notification_id)


@notifications_router.put("/mark-all-read", status_code=204)
def mark_all_as_read(
    user: User = Depends(require_roles("USER", "DEPARTMENT_ADMIN")),
    db: Session = Depends(get_db),
):
    notification_service.mark_all_user_as_read(db, user.id)


@notifications_router.put("/department/mark-all-read", status_code=204)
def mark_all_department_as_read(
    user: User = Depends(require_roles("USER", "DEPARTMENT_ADMIN")),
    db: Session = Depends(get_db),
):
    if not user.department_id:
        raise HTTPException(status_code=400, detail="Department not found")
    notification_service.mark_all_department_as_read(db, user.department_id)
