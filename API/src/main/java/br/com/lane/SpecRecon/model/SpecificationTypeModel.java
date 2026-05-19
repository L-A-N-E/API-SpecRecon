package br.com.lane.SpecRecon.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidade de domínio para tipos de especificação.
 * <p>
 * Define o tipo, categoria e regras de dados das especificações.
 * </p>
 */
@Entity
@Table(name = "Specification_Type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SpecificationTypeModel {
    /** Identificador do tipo de especificação. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nome do tipo (único). */
    @Column(nullable = false, unique = true)
    private String name;

    /** Categoria do tipo. */
    @Column(nullable = false)
    private String category;

    /** Descrição do tipo. */
    @Column(nullable = false)
    private String description;

    /** Tipo de dado associado à especificação. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DataType data_type;

    /** Unidade padrão recomendada para o tipo (quando aplicável). */
    @ManyToOne // Varias Especificacoes podem ter uma unidade em comum
    @JoinColumn(name = "default_unit_id")
    private UnitModel defaultUnit;
}
