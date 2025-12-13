import '../styles/Analytics.css';
import WellnessGauge from "../components/WellnessGauge.jsx";
import GoalProgressBar from "../components/GoalProgressBar.jsx";

export default function Analytics({ ctx }) {
  const { loadSummary, summary } = ctx;
  return (
    <section style={{ border:"1px solid #ddd", padding:16, borderRadius:12, marginTop:16 }}>
      <h2>Analytics</h2>
      <>
        <button onClick={loadSummary}>Load BMI & Wellness</button>
        {summary && (
          <div style={{ display: "grid", gridTemplateColumns: "1fr 2fr", gap: 16, marginTop: 12 }}>
            <div>
              <WellnessGauge value={summary?.scores?.wellness ?? 0} />
              <div style={{ marginTop: 8 }}>
                <b>BMI:</b> {summary?.bmi?.value} ({summary?.bmi?.classification})
              </div>

              <div style={{ marginTop: 12 }}>
                <GoalProgressBar
                  percent={summary?.goal?.progress?.percent ?? 0}
                  remainingKg={summary?.goal?.progress?.remainingKg ?? null}
                />
              </div>
            </div>
            <pre style={{ background: "#f7f7f7", padding: 12, borderRadius: 8 }}>
              {JSON.stringify(summary, null, 2)}
            </pre>
          </div>
        )}
      </>
    </section>
  );
}
