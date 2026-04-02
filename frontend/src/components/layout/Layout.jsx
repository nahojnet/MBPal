import { NavLink } from 'react-router-dom';
import './Layout.css';

const navItems = [
  { label: 'Dashboard', path: '/dashboard' },
  {
    label: 'Palettisation',
    children: [
      { label: 'Lancer', path: '/palletization/launch' },
      { label: 'Historique', path: '/palletization/history' },
      { label: 'Comparer', path: '/palletization/compare' },
    ],
  },
  {
    label: 'Regles',
    children: [
      { label: 'Liste des regles', path: '/rules' },
      { label: 'Creer une regle', path: '/rules/create' },
      { label: 'Rulesets', path: '/rulesets' },
    ],
  },
  {
    label: 'Referentiels',
    children: [
      { label: 'Produits', path: '/referentials/products' },
      { label: 'Supports', path: '/referentials/supports' },
    ],
  },
];

function Layout({ children }) {
  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="sidebar-header">
          <h2>MBPal</h2>
          <p>Palettisation</p>
        </div>
        <nav className="sidebar-nav">
          {navItems.map((item) =>
            item.children ? (
              <div key={item.label} className="nav-group">
                <span className="nav-group-label">{item.label}</span>
                {item.children.map((child) => (
                  <NavLink
                    key={child.path}
                    to={child.path}
                    className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
                  >
                    {child.label}
                  </NavLink>
                ))}
              </div>
            ) : (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
              >
                {item.label}
              </NavLink>
            )
          )}
        </nav>
      </aside>
      <main className="main-content">
        {children}
      </main>
    </div>
  );
}

export default Layout;
