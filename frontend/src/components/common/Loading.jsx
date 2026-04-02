function Loading({ message = 'Chargement...' }) {
  return (
    <div style={{ textAlign: 'center', padding: '40px', color: 'var(--gray-500)' }}>
      <p>{message}</p>
    </div>
  );
}

export default Loading;
