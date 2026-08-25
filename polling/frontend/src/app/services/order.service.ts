import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export type OrderType = 'INTERNAL' | 'EXTERNAL';
export type OrderStatus = 'PENDING' | 'SUCCESS' | 'REJECTED';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly httpClient = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/v1/orders';

  public initializeOrder(
    orderNumber: number,
    type: OrderType,
  ): Observable<string> {
    const params = new HttpParams()
      .set('orderNumber', orderNumber.toString())
      .set('type', type);

    return this.httpClient.post(this.baseUrl + '/init', null, {
      params,
      responseType: 'text',
    });
  }

  shortPoll(orderNumber: number): Observable<OrderStatus> {
    return this.httpClient.get<OrderStatus>(
      `${this.baseUrl}/${orderNumber}/short-poll`,
    );
  }

  longPoll(orderNumber: number): Observable<OrderStatus> {
    return this.httpClient.get<OrderStatus>(
      `${this.baseUrl}/${orderNumber}/long-poll`,
    );
  }
}
