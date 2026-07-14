from sqlalchemy.orm import Session

from app.auth import hash_password
from app.models import User


def register_user(db: Session, name: str, email: str, password: str) -> User:
    if db.query(User).filter(User.email == email).first():
        raise ValueError("Email already registered")

    user = User(name=name, email=email, password=hash_password(password), role="USER")
    db.add(user)
    db.commit()
    db.refresh(user)
    return user


def login_user(db: Session, email: str, password: str) -> User:
    from app.auth import verify_password

    user = db.query(User).filter(User.email == email).first()
    if not user or not verify_password(password, user.password):
        raise ValueError("Invalid credentials")
    return user


def get_user_by_email(db: Session, email: str) -> User | None:
    return db.query(User).filter(User.email == email).first()
