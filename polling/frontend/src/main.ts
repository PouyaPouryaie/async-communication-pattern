import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { OrderPollingComponent } from './app/order-polling.component';

bootstrapApplication(OrderPollingComponent, appConfig)
  .catch((err) => console.error(err));