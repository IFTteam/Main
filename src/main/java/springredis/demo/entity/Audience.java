package springredis.demo.entity;

import lombok.Data;
import springredis.demo.entity.activeEntity.ActiveNode;
import springredis.demo.entity.base.BaseEntity;

import javax.persistence.*;


import java.sql.Date;
import java.util.Set;

@Entity
@Data
public class Audience extends BaseEntity {
    @Id
    @GeneratedValue    
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;

    private Date birthday;
    private String source;

    @ManyToOne(targetEntity = Node.class)
    @JoinColumn(name="audience_node_id",referencedColumnName = "id")
    private Node node;
    
    @OneToMany(mappedBy = "audience", fetch = FetchType.LAZY,
            cascade = CascadeType.PERSIST, targetEntity = Transmission.class)
    private Set<Transmission> transmissions;
}
