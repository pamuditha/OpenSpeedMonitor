import {NgModule} from '@angular/core';
import {SetupDashboardComponent} from './setup-dashboard.component';
import {PageService} from "./services/page.service";
import {PageComponent} from './components/page/page.component';
import {PageListComponent} from './components/page-list/page-list.component';
import {ScriptListComponent} from './components/script-list/script-list.component';
import {ScriptService} from "./services/script.service";
import {ScriptComponent} from "./components/script/script.component";
import {JobGroupComponent} from "./components/job-group/job-group/job-group.component";
import {JobGroupListComponent} from "./components/job-group-list/job-group-list/job-group-list.component";
import {SharedModule} from "../shared/shared.module";
import {TranslationModule} from "../translation/translation.module";
import {HttpClientModule} from "@angular/common/http";

@NgModule({
  imports: [
    SharedModule,
    HttpClientModule,
    TranslationModule
  ],
  declarations:
    [SetupDashboardComponent, JobGroupComponent, JobGroupListComponent, PageComponent, PageListComponent, ScriptComponent, ScriptListComponent],
  providers: [
    {
      provide: 'components',
      useValue: [SetupDashboardComponent],
      multi: true
    }, PageService, ScriptService
  ],
  entryComponents: [SetupDashboardComponent]
})
export class SetupDashboardModule {
}
