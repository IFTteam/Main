package springredis.demo.entity;

import java.sql.Date;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import lombok.Data;
import springredis.demo.entity.base.BaseEntity;

@Entity
@Data
public class Audience extends BaseEntity {
    @Id
    @GeneratedValue
    private Long id;
    private String email;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    private String phone;
    private String address;

    private Date birthday;
    private String source;

    @ManyToOne(targetEntity = Node.class)
    @JoinColumn(name = "audience_node_id", referencedColumnName = "id")
    private Node node;

    @OneToMany(mappedBy = "audience", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST, targetEntity = Transmission.class)
    private Set<Transmission> transmissions;
}
