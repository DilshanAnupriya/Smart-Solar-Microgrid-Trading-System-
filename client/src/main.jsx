/*
 * File:        main.jsx
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Application
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-20
 * Description: Entry point of the React web application. Loads Bootstrap 5,
 *              installs the router and the authentication provider, and mounts
 *              the application into the page.
 */

import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';

// Bootstrap 5 styles, then the project's own overrides
import 'bootstrap/dist/css/bootstrap.min.css';

// Bootstrap's JavaScript bundle, used by the responsive navigation bar toggle
import 'bootstrap/dist/js/bootstrap.bundle.min.js';

import './index.css';

import App from './App';
import { AuthProvider } from './auth/AuthContext';
import { ToastProvider } from './toast/ToastContext';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    {/* AuthProvider must sit inside the router so guards can navigate, and
        ToastProvider wraps everything so any page can raise a notification */}
    <BrowserRouter>
      <ToastProvider>
        <AuthProvider>
          <App />
        </AuthProvider>
      </ToastProvider>
    </BrowserRouter>
  </StrictMode>,
);
