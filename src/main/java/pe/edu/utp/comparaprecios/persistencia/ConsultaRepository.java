package pe.edu.utp.comparaprecios.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsultaRepository extends JpaRepository<ConsultaRealizada, Long> {

    List<ConsultaRealizada> findTop20ByOrderByFechaConsultaDesc();
}
