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

          <div style={{ marginTop: 12 }}>
            <div>
              <WellnessGauge value={summary?.scores?.wellness ?? 0} />
              <div style={{ marginTop: 8 }}>
                <b>BMI:</b> {summary?.bmi?.value} ({summary?.bmi?.classification})
              </div>
            </div>
            <div style={{ marginTop: 12 }}>
              <GoalProgressBar
                percent={summary?.goal?.progress?.percent ?? 0}
                remainingKg={summary?.goal?.progress?.remainingKg ?? null}
              />
            </div>
            <div style={{ fontSize: 12, color: "#777", marginTop: 4 }}>
              Each segment = 5% milestone
            </div>
            {typeof summary?.goal?.progress?.milestones5pct === 'number' && (
              <div style={{ fontSize: 12, color: "#555", marginTop: 4 }}>
                Milestones achieved: {summary.goal.progress.milestones5pct}
              </div>
            )}
          </div>
        )}
      </>
    </section>
  );
}
