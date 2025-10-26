export default function GoalProgressBar({ percent = 0, remainingKg = null }) {
  const p = Math.max(0, Math.min(100, Number(percent) || 0));
  const ticks = Array.from({ length: 21 }, (_, i) => i * 5); // 0..100 шаг 5%

  return (
    <div style={{ width: "100%", padding: "8px 0" }}>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4, fontSize: 14 }}>
        <span><b>Goal progress:</b> {p.toFixed(1)}%</span>
        <span style={{ color: "#555" }}>
          {p >= 100 ? "Goal reached 🎉" : (remainingKg != null ? `${remainingKg} kg left` : "")}
        </span>
      </div>

      <div style={{ position: "relative", height: 14, background: "#eee", borderRadius: 999 }}>
        {/* заполненная часть */}
        <div
          style={{
            width: `${p}%`,
            height: "100%",
            borderRadius: 999,
            transition: "width .3s ease",
            background: `linear-gradient(90deg, #9aa5ff, #7f6cff)` // мягкий фиолетовый
          }}
        />

        {/* риски каждые 5% */}
        {ticks.map(t => (
          <div
            key={t}
            title={`${t}%`}
            style={{
              position: "absolute",
              left: `calc(${t}% - 1px)`,
              top: 0,
              width: 2,
              height: 14,
              background: t <= p ? "rgba(0,0,0,0.25)" : "rgba(0,0,0,0.12)",
              borderRadius: 1
            }}
          />
        ))}
      </div>

      {/* подписи 0 / 50 / 100 снизу для ориентира */}
      <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, color: "#777", marginTop: 4 }}>
        <span>0%</span><span>50%</span><span>100%</span>
      </div>
    </div>
  );
}
