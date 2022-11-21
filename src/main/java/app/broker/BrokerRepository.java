package app.broker;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BrokerRepository extends JpaRepository<Broker, Long> , HasCountById {
    int countById(Long id);
    int countByName(String name);
}
