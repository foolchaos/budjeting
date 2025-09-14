package com.example.budjeting.view;

import com.example.budjeting.entity.Cfo;
import com.example.budjeting.entity.Mvz;
import com.example.budjeting.repository.CfoRepository;
import com.example.budjeting.repository.MvzRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "cfo", layout = MainLayout.class)
@PageTitle("ЦФО/МВЗ")
public class CfoMvzView extends HorizontalLayout {

    private final CfoRepository cfoRepository;
    private final MvzRepository mvzRepository;

    private final Grid<Cfo> cfoGrid = new Grid<>(Cfo.class);
    private final Grid<Mvz> mvzGrid = new Grid<>(Mvz.class);

    private final Binder<Cfo> cfoBinder = new Binder<>(Cfo.class);
    private final Binder<Mvz> mvzBinder = new Binder<>(Mvz.class);

    private Cfo currentCfo;
    private Mvz currentMvz;

    public CfoMvzView(@Autowired CfoRepository cfoRepository, @Autowired MvzRepository mvzRepository) {
        this.cfoRepository = cfoRepository;
        this.mvzRepository = mvzRepository;

        configureCfoGrid();
        configureMvzGrid();

        add(new VerticalLayout(cfoGrid, createCfoForm()), new VerticalLayout(mvzGrid, createMvzForm()));
        updateCfoList();
    }

    private void configureCfoGrid() {
        cfoGrid.setColumns("code", "name");
        cfoGrid.asSingleSelect().addValueChangeListener(e -> selectCfo(e.getValue()));
    }

    private void configureMvzGrid() {
        mvzGrid.setColumns("code", "name");
        mvzGrid.asSingleSelect().addValueChangeListener(e -> editMvz(e.getValue()));
    }

    private FormLayout createCfoForm() {
        TextField code = new TextField("Код");
        TextField name = new TextField("Наименование");
        cfoBinder.bind(code, Cfo::getCode, Cfo::setCode);
        cfoBinder.bind(name, Cfo::getName, Cfo::setName);
        Button save = new Button("Сохранить", e -> saveCfo());
        Button add = new Button("Добавить", e -> selectCfo(new Cfo()));
        Button delete = new Button("Удалить", e -> deleteCfo());
        return new FormLayout(code, name, new HorizontalLayout(save, add, delete));
    }

    private FormLayout createMvzForm() {
        TextField code = new TextField("Код");
        TextField name = new TextField("Наименование");
        mvzBinder.bind(code, Mvz::getCode, Mvz::setCode);
        mvzBinder.bind(name, Mvz::getName, Mvz::setName);
        Button save = new Button("Сохранить", e -> saveMvz());
        Button add = new Button("Добавить", e -> editMvz(new Mvz()));
        Button delete = new Button("Удалить", e -> deleteMvz());
        return new FormLayout(code, name, new HorizontalLayout(save, add, delete));
    }

    private void selectCfo(Cfo cfo) {
        currentCfo = cfo;
        cfoBinder.setBean(cfo);
        if (cfo != null) {
            mvzGrid.setItems(cfo.getMvzs());
        } else {
            mvzGrid.setItems();
        }
    }

    private void editMvz(Mvz mvz) {
        currentMvz = mvz;
        mvzBinder.setBean(mvz);
    }

    private void saveCfo() {
        cfoRepository.save(currentCfo);
        updateCfoList();
    }

    private void deleteCfo() {
        if (currentCfo != null && currentCfo.getId() != null) {
            cfoRepository.delete(currentCfo);
            selectCfo(null);
            updateCfoList();
        }
    }

    private void saveMvz() {
        currentMvz.setCfo(currentCfo);
        mvzRepository.save(currentMvz);
        selectCfo(cfoRepository.findById(currentCfo.getId()).orElse(null));
    }

    private void deleteMvz() {
        if (currentMvz != null && currentMvz.getId() != null) {
            mvzRepository.delete(currentMvz);
            selectCfo(cfoRepository.findById(currentCfo.getId()).orElse(null));
        }
    }

    private void updateCfoList() {
        cfoGrid.setItems(cfoRepository.findAll());
    }
}
