function ErrorMessage({ message }) {
  if (!message) return null;
  return (
    <div style={{
      background: 'var(--danger-light)',
      color: 'var(--danger)',
      padding: '12px 16px',
      borderRadius: 'var(--radius)',
      marginBottom: '16px',
    }}>
      {message}
    </div>
  );
}

export default ErrorMessage;
