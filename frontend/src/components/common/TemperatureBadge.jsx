import { TEMPERATURE_COLORS } from '../../utils/constants';

function TemperatureBadge({ type }) {
  const color = TEMPERATURE_COLORS[type] || '#6B7280';
  return (
    <span className="badge" style={{ background: `${color}20`, color }}>
      {type}
    </span>
  );
}

export default TemperatureBadge;
