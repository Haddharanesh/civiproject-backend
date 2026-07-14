from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.database import get_db
from app.deps import require_roles
from app.schemas import AdminRequest, ComplaintOut, DepartmentCreate, DepartmentOut, UserOut
from app.services import superadmin_service

router = APIRouter(prefix="/api/superadmin", tags=["superadmin"])


@router.get("/users", response_model=list[UserOut])
def get_all_users(_: object = Depends(require_roles("SUPER_ADMIN")), db: Session = Depends(get_db)):
    return superadmin_service.get_all_users(db)


@router.put("/promote/{user_id}", response_model=UserOut)
def promote_user(
    user_id: int,
    _: object = Depends(require_roles("SUPER_ADMIN")),
    db: Session = Depends(get_db),
):
    try:
        return superadmin_service.promote_user(db, user_id)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@router.put("/demote/{user_id}", response_model=UserOut)
def demote_user(
    user_id: int,
    _: object = Depends(require_roles("SUPER_ADMIN")),
    db: Session = Depends(get_db),
):
    try:
        return superadmin_service.demote_user(db, user_id)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@router.delete("/users/{user_id}", status_code=204)
def delete_user(
    user_id: int,
    _: object = Depends(require_roles("SUPER_ADMIN")),
    db: Session = Depends(get_db),
):
    try:
        superadmin_service.delete_user(db, user_id)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@router.get("/departments", response_model=list[DepartmentOut])
def get_all_departments(
    _: object = Depends(require_roles("SUPER_ADMIN")),
    db: Session = Depends(get_db),
):
    return superadmin_service.get_all_departments(db)


@router.post("/departments", response_model=DepartmentOut)
def create_department(
    payload: DepartmentCreate,
    _: object = Depends(require_roles("SUPER_ADMIN")),
    db: Session = Depends(get_db),
):
    return superadmin_service.create_department(db, payload.name, payload.description)


@router.delete("/departments/{department_id}", status_code=204)
def delete_department(
    department_id: int,
    _: object = Depends(require_roles("SUPER_ADMIN")),
    db: Session = Depends(get_db),
):
    try:
        superadmin_service.delete_department(db, department_id)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@router.post("/create-admin", response_model=UserOut)
def create_department_admin(
    payload: AdminRequest,
    _: object = Depends(require_roles("SUPER_ADMIN")),
    db: Session = Depends(get_db),
):
    try:
        return superadmin_service.create_department_admin(
            db, payload.name, payload.email, payload.password, payload.departmentId
        )
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@router.get("/complaints", response_model=list[ComplaintOut])
def get_all_complaints(
    _: object = Depends(require_roles("SUPER_ADMIN")),
    db: Session = Depends(get_db),
):
    return superadmin_service.get_all_complaints(db)


@router.delete("/complaints/{complaint_id}", status_code=204)
def delete_complaint(
    complaint_id: int,
    _: object = Depends(require_roles("SUPER_ADMIN")),
    db: Session = Depends(get_db),
):
    try:
        superadmin_service.delete_complaint(db, complaint_id)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
