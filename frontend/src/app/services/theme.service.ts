import { Injectable, signal, effect } from '@angular/core';

export type AppTheme = 'light' | 'dark';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly THEME_KEY = 'connectly_theme';
  readonly currentTheme = signal<AppTheme>('dark');

  constructor() {
    const saved = localStorage.getItem(this.THEME_KEY) as AppTheme;
    if (saved === 'light' || saved === 'dark') {
      this.currentTheme.set(saved);
    } else {
      const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
      this.currentTheme.set(prefersDark ? 'dark' : 'light');
    }

    effect(() => {
      const theme = this.currentTheme();
      localStorage.setItem(this.THEME_KEY, theme);
      if (theme === 'dark') {
        document.body.classList.add('dark-theme');
        document.body.classList.remove('light-theme');
      } else {
        document.body.classList.add('light-theme');
        document.body.classList.remove('dark-theme');
      }
    });
  }

  toggleTheme(): void {
    this.currentTheme.update(theme => (theme === 'dark' ? 'light' : 'dark'));
  }
}
