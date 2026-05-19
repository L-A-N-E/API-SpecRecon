package br.com.lane.SpecRecon.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entidade de domínio para unidades de medida.
 * <p>
 * Representa unidades usadas nas especificações.
 * </p>
 */
@Entity
@Table(name = "Unit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UnitModel {
    /** Identificador da unidade. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nome da unidade. */
    @Column(nullable = false)
    private String name;

    /** Símbolo da unidade (único). */
    @Column(nullable = false, unique = true)
    private String symbol;

    /** Dimensão física associada à unidade. */
    @Enumerated(EnumType.STRING)
    private Dimension dimension;

    /** Fator de conversão para a unidade base da dimensão. */
    @Column(nullable = false)
    private BigDecimal conversion_factor_to_base; // Fator de conversao para unidade base da dimensao
}
