package com.example.budget;

import com.example.budget.domain.*;
import com.example.budget.repo.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration
public class DemoData {
    @Bean
    CommandLineRunner init(BdzRepository bdzRepo, BoRepository boRepo, ZgdRepository zgdRepo,
                           CfoRepository cfoRepo, MvzRepository mvzRepo, RequestRepository reqRepo,
                           ContractRepository contractRepo) {
        return args -> {
            if (bdzRepo.count() == 0) {
                Bdz root = new Bdz();
                root.setCode("BDZ-ROOT");
                root.setName("Корневая статья");
                bdzRepo.save(root);

                Bdz child = new Bdz();
                child.setCode("BDZ-1");
                child.setName("Статья 1");
                child.setParent(root);
                bdzRepo.save(child);

                Zgd zgd = new Zgd();
                zgd.setFullName("Иванов И.И.");
                zgd.setDepartment("Финансы");
                zgd.setBdz(child);
                zgdRepo.save(zgd);

                Bo bo1 = new Bo();
                bo1.setCode("BO-1");
                bo1.setName("Статья БО 1");
                bo1.setBdz(child);
                boRepo.save(bo1);
            }

            if (cfoRepo.count() == 0) {
                Cfo cfo = new Cfo();
                cfo.setCode("CFO-1");
                cfo.setName("ЦФО 1");
                cfoRepo.save(cfo);

                Mvz mvz = new Mvz();
                mvz.setCode("MVZ-1");
                mvz.setName("МВЗ 1");
                mvz.setCfo(cfo);
                mvzRepo.save(mvz);
            }

            if (reqRepo.count() == 0) {
                // Создадим пустую заявку, чтобы интерфейс имел пример
                Request r = new Request();
                r.setVgo("ВГО-Пример");
                r.setAmount(new BigDecimal("1.23"));
                r.setAmountNoVat(new BigDecimal("1.02"));
                r.setSubject("Поставка оборудования");
                r.setPeriod("2025-09");
                r.setInputObject(true);
                r.setProcurementMethod("44-ФЗ");
                // Привяжем к ранее созданным сущностям, с учетом ограничений 1:1
                Bdz anyBdz = bdzRepo.findAll().stream().filter(b -> !"BDZ-ROOT".equals(b.getCode())).findFirst().orElse(null);
                if (anyBdz != null) {
                    r.setBdz(anyBdz);
                    if (anyBdz.getZgd() != null) r.setZgd(anyBdz.getZgd());
                    // BO выбираем первую
                    java.util.List<Bo> bos = boRepo.findByBdzId(anyBdz.getId());
                if (!bos.isEmpty()) { r.setBo(bos.get(0)); }
                }
                Cfo anyCfo = cfoRepo.findAll().stream().findFirst().orElse(null);
                if (anyCfo != null) {
                    r.setCfo(anyCfo);
                    java.util.List<Mvz> mvzs = mvzRepo.findByCfoId(anyCfo.getId());
                Mvz anyMvz = mvzs.isEmpty()? null : mvzs.get(0);
                    if (anyMvz != null) r.setMvz(anyMvz);
                }

                Contract c = new Contract();
                c.setName("ООО «Поставщик»");
                c.setInternalNumber("INT-001");
                c.setExternalNumber("EXT-2025/01");
                c.setContractDate(LocalDate.now());
                c.setResponsible("Петров П.П.");
                r.setContract(c);

                reqRepo.save(r);
            }
        };
    }
}
