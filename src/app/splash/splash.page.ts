import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-splash',
  templateUrl: './splash.page.html',
  styleUrls: ['./splash.page.scss'],
})
export class SplashPage implements OnInit, OnDestroy {

  interval!: any;
  backgroundColor: string = 'black';

  constructor(private router: Router) { }

  ngOnInit() {
    setTimeout(() => this.router.navigateByUrl('home'), 2000);
    this.startColorInterval();
    this.backgroundColor = this.getRandomColor();
  }

  ngOnDestroy(): void {
    clearInterval(this.interval);
  }

  startColorInterval() {
    const ionContent = document.querySelector('ion-content');
    this.interval = setInterval(() => {
      ionContent?.style.setProperty('--background', this.getRandomColor());
    }, 100);
  }

  getRandomColor() {
    const letters = '0123456789ABCDEF';
    let color = '#';
    for (let i = 0; i < 6; i++) {
      color += letters[Math.floor(Math.random() * 16)];
    }
    return color;
  }

}
