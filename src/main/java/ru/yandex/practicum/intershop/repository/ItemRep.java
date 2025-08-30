package ru.yandex.practicum.intershop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.yandex.practicum.intershop.model.Item;

import java.util.Set;

public interface ItemRep extends JpaRepository<Item, Long> {

    @Query(value = """
                   select i.id,
                          i.order_id,
                          w.id ware_id,
                          coalesce(i.count, 0) count,
                          coalesce(i.created_at, w.created_at) created_at
                     from wares w
                     left join items i on w.id = i.ware_id
                      and i.order_id = (select max(id) from orders o where o.stat = 'NEW')
                    order by i.id nulls last, w.title
                   """,
            nativeQuery = true)
    Set<Item> findAllItems();

    @Query(value = """
                   select i.*
                     from items i
                    where i.order_id = (select max(id) from orders o where o.stat = 'NEW')
                      and i.ware_id = :wareId
                   """,
            nativeQuery = true)
    Item getByWare_Id(Long wareId);
}
