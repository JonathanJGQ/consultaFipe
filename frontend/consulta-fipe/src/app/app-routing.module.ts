import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { ConsultaComponent } from './consulta/consulta.component';

const routes: Routes = [
  { path: '', redirectTo:'consulta', pathMatch: 'full'},
  { path: 'consulta', component: ConsultaComponent}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
