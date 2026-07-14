package com.civic.civicbackend.dto;

public class AdminRequest {
    private String name;
    private String email;
    private String password;
    private Long departmentId;
    public String getName() {
        return name;
    }
    public AdminRequest(String name, String email, String password, Long departmentId) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.departmentId = departmentId;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public Long getDepartmentId() {
        return departmentId;
    }
    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    
}