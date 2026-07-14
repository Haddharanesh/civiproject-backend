from sqlalchemy.orm import Session

from app.models import Complaint, Vote

ALREADY_SUPPORTED_MSG = "You have already supported this complaint."
VOTE_ADDED_MSG = "Your support has been added to this complaint."


def has_voted(db: Session, user_id: int, complaint_id: int) -> bool:
    return (
        db.query(Vote)
        .filter(Vote.user_id == user_id, Vote.complaint_id == complaint_id)
        .first()
        is not None
    )


def try_upvote(db: Session, user_id: int, complaint_id: int) -> tuple[Complaint, bool, str]:
    """Attempt to add one vote. Returns (complaint, vote_added, message)."""
    complaint = db.query(Complaint).filter(Complaint.id == complaint_id).first()
    if not complaint:
        raise ValueError("Complaint not found")

    if has_voted(db, user_id, complaint_id):
        return complaint, False, ALREADY_SUPPORTED_MSG

    db.add(Vote(user_id=user_id, complaint_id=complaint_id))
    complaint.upvotes = (complaint.upvotes or 0) + 1
    db.commit()
    db.refresh(complaint)
    return complaint, True, VOTE_ADDED_MSG


def upvote(db: Session, user_id: int, complaint_id: int) -> Complaint:
    """Strict upvote used by /api/votes — raises if already voted."""
    complaint, added, message = try_upvote(db, user_id, complaint_id)
    if not added:
        raise ValueError(message)
    return complaint
