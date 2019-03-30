import { Component, OnInit } from '@angular/core';
import { ConsultaFipeService } from '../service/consulta-fipe.service'

@Component({
  selector: 'app-consulta',
  templateUrl: './consulta.component.html',
  styleUrls: ['./consulta.component.css']
})
export class ConsultaComponent implements OnInit {

  constructor(private service: ConsultaFipeService) { }

  marcas = [];
  veiculos = []
  detalhes = []
  idMarca = null;
  idVeiculo = null;
  ngOnInit() {
    this.service.getMarcas().subscribe(result => {
      this.marcas = result;
    });
  }

  onChangeMarca(){
    this.service.getVeiculos(this.idMarca).subscribe(result => {
      this.veiculos = result;
    });
  }

  buscarFipe(){
    this.service.getDetalhe(this.idMarca, this.idVeiculo).subscribe(result => {
      this.detalhes = result;
    });
  }

}
