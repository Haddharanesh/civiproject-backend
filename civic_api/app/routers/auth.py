from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.auth import create_access_token
from app.database import get_db
from app.schemas import UserLogin, UserOut, UserRegister
from app.services import auth_service

router = APIRouter(prefix="/api/auth", tags=["auth"])


@router.post("/register")
def register(payload: UserRegister, db: Session = Depends(get_db)):
    try:
        user = auth_service.register_user(db, payload.name, payload.email, payload.password)
        return UserOut.model_validate(user)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail={"error": str(exc)}) from exc


@router.post("/login")
def login(payload: UserLogin, db: Session = Depends(get_db)):
    try:
        user = auth_service.login_user(db, payload.email, payload.password)
        token = create_access_token(user.email, user.role)
        return {
            "token": token,
            "user": {
                "id": user.id,
                "name": user.name,
                "email": user.email,
                "role": user.role,
            },
        }
    except ValueError as exc:
        raise HTTPException(status_code=400, detail={"error": "Invalid credentials"}) from exc
