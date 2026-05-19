package br.com.lane.SpecRecon.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entidade de domínio para especificações de veículos.
 * <p>
 * Representa valores de especificações associadas a um veículo.
 * </p>
 */
@Entity
@Table(name = "Vehicle_Specification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSpecificationModel {
    /** Identificador da especificação do veículo. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Veículo associado à especificação. */
    @ManyToOne // Varias especificacoes para um veiculo
    @JoinColumn(name = "vehicle_id", nullable = false)
    private VehicleModel vehicle;

    /** Tipo de especificação associado. */
    @ManyToOne // Varias especificacoes podem ser do mesmo tipo
    @JoinColumn(name = "specification_type_id", nullable = false)
    private SpecificationTypeModel specificationType;

    /** Unidade usada para o valor numérico (quando aplicável). */
    @ManyToOne // Varias especificacoes podem usar a mesma unidade
    @JoinColumn(name = "unit_id")
    private UnitModel unit;

    /** Valor numérico da especificação. */
    @Column
    private BigDecimal value_numeric;

    /** Valor em texto da especificação. */
    @Column
    private String value_text;

    /** Indicador de disponibilidade da especificação. */
    @Column
    private Boolean is_available;
}
