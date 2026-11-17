import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import App from './App';

describe('App Component', () => {
  it('renders the header with brand title', () => {
    render(<App />);
    expect(screen.getByText('Vibe')).toBeInTheDocument();
    expect(screen.getByText('Commerce')).toBeInTheDocument();
  });

  it('renders the Phase 0 badge', () => {
    render(<App />);
    expect(screen.getByText('Phase 0: Scaffolding')).toBeInTheDocument();
  });

  it('renders base component sections', () => {
    render(<App />);
    expect(screen.getByText('Button Variants & States')).toBeInTheDocument();
    expect(screen.getByText('Input Component')).toBeInTheDocument();
    expect(screen.getByText('Badges & Statuses')).toBeInTheDocument();
  });
});

