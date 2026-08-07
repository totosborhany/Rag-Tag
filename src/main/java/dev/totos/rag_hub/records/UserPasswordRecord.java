package dev.totos.rag_hub.records;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserPasswordRecord(
        @NotBlank(message = "oldPassword is required")
        @Size(min = 8, message = "oldPassword must be at least 8 characters long")
        @Pattern(
                regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_]).{8,}$",
                message = "oldPassword must contain at least one digit, one lowercase letter, one uppercase letter, and one special character"
        )
        String oldPassword
        ,
        @NotBlank(message = "newPassword is required")
        @Size(min = 8, message = "newPassword must be at least 8 characters long")
        @Pattern(
                regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_]).{8,}$",
                message = "newPassword must contain at least one digit, one lowercase letter, one uppercase letter, and one special character"
        )
        String newPassword,
        @NotBlank(message = "confirmNewPassword is required")
        @Size(min = 8, message = "ConfirmNewPassword must be at least 8 characters long")
        @Pattern(
                regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_]).{8,}$",
                message = "confirmNewPassword must contain at least one digit, one lowercase letter, one uppercase letter, and one special character"
        )
        String confirmNewPassword
        ){

}
