import '../styles/Log.css';
export default function Log({ ctx }) {
  const { log } = ctx;
  return (
    <section style={{ border: "1px solid #ddd", padding: 16, borderRadius: 12, marginTop: 16 }}>
      <h3>Log</h3>
      <textarea readOnly rows={10} style={{ width: "100%", fontFamily: "monospace" }} value={log} />
    </section>
  );
}
