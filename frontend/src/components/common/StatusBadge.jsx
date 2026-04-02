import { STATUS_COLORS } from '../../utils/constants';

function StatusBadge({ status }) {
  const config = STATUS_COLORS[status] || STATUS_COLORS.PENDING;
  return (
    <span className="badge" style={{ background: config.bg, color: config.text }}>
      {config.label}
    </span>
  );
}

export default StatusBadge;
