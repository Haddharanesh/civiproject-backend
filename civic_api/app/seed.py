from sqlalchemy.orm import Session

from app.auth import hash_password
from app.models import Department, User

DEPARTMENTS = [
    ("Public Works Department", "Roads, bridges, and public infrastructure"),
    ("Sanitation Department", "Waste management and cleanliness"),
    ("Electricity Board", "Power and street lighting"),
    ("Water Supply Department", "Water, drainage, and sewage"),
    ("General Municipal Department", "General civic issues"),
]


def seed_database(db: Session):
    for name, description in DEPARTMENTS:
        if not db.query(Department).filter(Department.name == name).first():
            db.add(Department(name=name, description=description))

    if not db.query(User).filter(User.email == "admin@civic.gov").first():
        db.add(
            User(
                name="Super Admin",
                email="admin@civic.gov",
                password=hash_password("admin123"),
                role="SUPER_ADMIN",
            )
        )

    # Demo department admin for quick testing
    sanitation = db.query(Department).filter(Department.name == "Sanitation Department").first()
    if sanitation and not db.query(User).filter(User.email == "deptadmin@civic.gov").first():
        db.add(
            User(
                name="Department Admin",
                email="deptadmin@civic.gov",
                password=hash_password("admin123"),
                role="DEPARTMENT_ADMIN",
                department_id=sanitation.id,
            )
        )

    db.commit()
