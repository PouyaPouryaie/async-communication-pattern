import { Component, OnDestroy, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription, of, interval } from 'rxjs';
import { expand, switchMap, takeWhile } from 'rxjs/operators';
import { OrderService, OrderType, OrderStatus } from './services/order.service';

@Component({
  selector: 'app-order-polling',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './order-polling.component.html',
  styleUrls: ['./order-polling.component.css']
})
export class OrderPollingComponent implements OnDestroy {
  private orderService = inject(OrderService);

  orderNumber: number | null = null;
  orderType: OrderType = 'INTERNAL';

  isPolling = signal<boolean>(false);
  currentStatus = signal<OrderStatus | null>(null);
  statusMessage = signal<string>('');
  errorMessage = signal<string>('');

  private pollingSub?: Subscription;

  ngOnDestroy(): void {
    this.stopPolling();
  }

  startOrderProcess() {
    if (!this.orderNumber) return;

    this.resetState();
    this.isPolling.set(true);
    this.statusMessage.set('Initializing order on server...');

    this.orderService.initializeOrder(this.orderNumber, this.orderType).subscribe({
      next: (responseInit) => {
        this.statusMessage.set(responseInit);
        if (this.orderType === 'INTERNAL') {
          this.initShortPolling();
        } else {
          this.initLongPolling();
        }
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Initialization failed.');
        this.isPolling.set(false);
      }
    });
  }

  private initShortPolling() {
    this.statusMessage.set('Short polling active (checking every 2s)...');
    
    this.pollingSub = interval(2000).pipe(
      switchMap(() => this.orderService.shortPoll(this.orderNumber!)),
      takeWhile((status) => status === 'PENDING', true)
    ).subscribe({
      next: (status) => {
        this.currentStatus.set(status);
        if (status !== 'PENDING') {
          this.handleTerminalState(status);
        }
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Short polling error occurred.');
        this.stopPolling();
      }
    });
  }

  private initLongPolling() {
    this.statusMessage.set('Long polling active (waiting for response)...');

    this.pollingSub = this.orderService.longPoll(this.orderNumber!).pipe(
      expand((status) => {
        if (status === 'PENDING') {
          return this.orderService.longPoll(this.orderNumber!);
        }
        return of();
      }),
      takeWhile((status) => status === 'PENDING', true)
    ).subscribe({
      next: (status) => {
        this.currentStatus.set(status);
        if (status !== 'PENDING') {
          this.handleTerminalState(status);
        }
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Long polling error occurred.');
        this.stopPolling();
      }
    });
  }

  private handleTerminalState(status: OrderStatus) {
    if (status === 'SUCCESS') {
      this.statusMessage.set('Order completed successfully!');
    } else if (status === 'REJECTED') {
      this.statusMessage.set('Order was rejected.');
    }
    this.stopPolling();
  }

  private stopPolling() {
    if (this.pollingSub) {
      this.pollingSub.unsubscribe();
    }
    this.isPolling.set(false);
  }

  private resetState() {
    this.stopPolling();
    this.currentStatus.set(null);
    this.statusMessage.set('');
    this.errorMessage.set('');
  }

  getStatusClass(status: OrderStatus): string {
    switch (status) {
      case 'SUCCESS': return 'status-success';
      case 'REJECTED': return 'status-rejected';
      case 'PENDING': return 'status-pending';
      default: return '';
    }
  }
}