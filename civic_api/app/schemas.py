from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


class DepartmentOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    name: str
    description: str | None = None


class UserOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    name: str
    email: str
    role: str
    department: DepartmentOut | None = None


class UserRegister(BaseModel):
    name: str
    email: str
    password: str


class UserLogin(BaseModel):
    email: str
    password: str


class ComplaintOut(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: int
    title: str | None = None
    category: str | None = None
    description: str | None = None
    latitude: float | None = None
    longitude: float | None = None
    status: str
    priority: str
    imageUrl: str | None = Field(None, validation_alias="image_url")
    upvotes: int
    remarks: str | None = None
    user: UserOut | None = None
    department: DepartmentOut | None = None
    createdAt: datetime | None = Field(None, validation_alias="created_at")
    updatedAt: datetime | None = Field(None, validation_alias="updated_at")


class NotificationOut(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: int
    userId: int | None = Field(None, validation_alias="user_id")
    departmentId: int | None = Field(None, validation_alias="department_id")
    message: str
    type: str | None = None
    complaintId: int | None = Field(None, validation_alias="complaint_id")
    read: bool = Field(validation_alias="is_read")
    createdAt: datetime | None = Field(None, validation_alias="created_at")


class AdminRequest(BaseModel):
    name: str
    email: str
    password: str
    departmentId: int


class DepartmentCreate(BaseModel):
    name: str
    description: str | None = None


class CreateComplaintResponse(BaseModel):
    """Add-on response for create/duplicate-detection flow."""

    isDuplicate: bool
    voteAdded: bool = False
    message: str
    complaint: ComplaintOut


class VoteResponse(BaseModel):
    voteAdded: bool
    message: str
    complaint: ComplaintOut
