import { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid } from "recharts";

export default function ActivityWeekChart({ byWeekdayMinutes = {} }) {
  const labels = {1:"Mon",2:"Tue",3:"Wed",4:"Thu",5:"Fri",6:"Sat",7:"Sun"};
  const data = Array.from({length:7}, (_,i)=>({
    day: labels[i+1],
    minutes: byWeekdayMinutes[i+1] || 0
  }));
  const max = Math.max(...data.map(d=>d.minutes), 60);
  return (
    <div style={{ width:"100%", height: 260 }}>
      <ResponsiveContainer>
        <BarChart data={data} margin={{ top: 8, right: 20, left: 0, bottom: 8 }}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="day" />
          <YAxis domain={[0, Math.ceil(max/30)*30]} unit=" min" />
          <Tooltip formatter={(v)=>[`${v} min`, "Minutes"]} />
          <Bar dataKey="minutes" name="Minutes" />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
