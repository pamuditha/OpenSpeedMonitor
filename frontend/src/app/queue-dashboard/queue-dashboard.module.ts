import {LOCALE_ID, NgModule} from '@angular/core';
import {QueueDashboardComponent} from './queue-dashboard.component';
import {SharedModule} from '../shared/shared.module';
import {RouterModule, Routes} from "@angular/router";
import {QueueDashboardEntryComponent} from "./queue-dashboard.entry-component";
import {QueueDashboardService} from "./services/queue-dashboard.service";
import {HttpClientModule} from "@angular/common/http";
import {LocationInfoListComponent} from "./components/location-info-list/location-info-list.component";

import { registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/de';
registerLocaleData(localeDe, 'de');

const QueueRoutes: Routes = [
  {path: '', component: QueueDashboardComponent},
];

@NgModule({
  imports: [
    RouterModule.forChild(QueueRoutes),
    SharedModule,
    HttpClientModule
  ],
  declarations: [
    QueueDashboardEntryComponent,
    QueueDashboardComponent,
    LocationInfoListComponent
  ],
  exports: [
  RouterModule
  ],
  providers: [
    {
      provide: 'components',
      useValue: [QueueDashboardEntryComponent],
      multi: true
    },
    {
      provide: LOCALE_ID, useValue: "de"
    },
    QueueDashboardService
  ],
  entryComponents: [QueueDashboardEntryComponent]
})
export class QueueDashboardModule { }
