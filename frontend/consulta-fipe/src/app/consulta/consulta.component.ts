import { Component, OnInit } from '@angular/core';
import { ConsultaFipeService } from '../service/consulta-fipe.service'
import Swal from 'sweetalert2';

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
    
    this.detalhes = [];
    Swal.fire({
      type: 'info',
      title: 'Aguarde...',
      showConfirmButton: false
    });
    this.service.getVeiculos(this.idMarca).subscribe(result => {
      Swal.close();
      this.veiculos = result;
    });
  }

  buscarFipe(){
    Swal.fire({
      type: 'info',
      title: 'Aguarde...',
      showConfirmButton: false
    });
    this.service.getDetalhe(this.idMarca, this.idVeiculo).subscribe(result => {
      Swal.close();
      this.detalhes = result;
    });
  }

}
