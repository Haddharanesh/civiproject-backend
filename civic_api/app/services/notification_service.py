from sqlalchemy.orm import Session

from app.models import Notification


def create_user_notification(db: Session, user_id: int, message: str, type_: str, complaint_id: int):
    notification = Notification(
        user_id=user_id,
        message=message,
        type=type_,
        complaint_id=complaint_id,
    )
    db.add(notification)
    db.commit()


def create_department_notification(
    db: Session, department_id: int, message: str, type_: str, complaint_id: int
):
    notification = Notification(
        department_id=department_id,
        message=message,
        type=type_,
        complaint_id=complaint_id,
    )
    db.add(notification)
    db.commit()


def get_user_notifications(db: Session, user_id: int) -> list[Notification]:
    return (
        db.query(Notification)
        .filter(Notification.user_id == user_id)
        .order_by(Notification.created_at.desc())
        .all()
    )


def get_department_notifications(db: Session, department_id: int) -> list[Notification]:
    return (
        db.query(Notification)
        .filter(Notification.department_id == department_id)
        .order_by(Notification.created_at.desc())
        .all()
    )


def get_unread_user_count(db: Session, user_id: int) -> int:
    return db.query(Notification).filter(Notification.user_id == user_id, Notification.is_read.is_(False)).count()


def get_unread_department_count(db: Session, department_id: int) -> int:
    return (
        db.query(Notification)
        .filter(Notification.department_id == department_id, Notification.is_read.is_(False))
        .count()
    )


def mark_as_read(db: Session, notification_id: int):
    notification = db.query(Notification).filter(Notification.id == notification_id).first()
    if notification:
        notification.is_read = True
        db.commit()


def mark_all_user_as_read(db: Session, user_id: int):
    notifications = db.query(Notification).filter(Notification.user_id == user_id).all()
    for notification in notifications:
        notification.is_read = True
    db.commit()


def mark_all_department_as_read(db: Session, department_id: int):
    notifications = db.query(Notification).filter(Notification.department_id == department_id).all()
    for notification in notifications:
        notification.is_read = True
    db.commit()
