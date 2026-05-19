package ma.smartfleet.backend.domain.model;

import lombok.*;
import jakarta.persistence.*;
import ma.smartfleet.backend.domain.model.enums.UserRole;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "clients", indexes = {
    @Index(name = "idx_client_company", columnList = "company_name")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Client extends User {
    
    @Column(name = "company_name")
    private String companyName;
    
    @Column
    private String businessAddress;
    
    @Column
    private String businessPhone;
    
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Order> orders = new HashSet<>();
    
    @Column(nullable = false)
    private Boolean verified = false;
    
    @Column
    private String firebaseDeviceToken;
    
    @PrePersist
    private void setRole() {
        this.setRole(UserRole.CLIENT);
    }
}
