// Genuinely computed (not mocked) open/closed status for a handful of major
// exchanges, based on their real trading-hour windows (local exchange time)
// compared against the current instant. Simplification: continuous session,
// no lunch-break/half-day handling (e.g. TSE's midday break) — acceptable
// for an at-a-glance dashboard widget, not a trading system of record.
const EXCHANGES = [
  { name: 'NYSE',     tz: 'America/New_York', open: [9, 30],  close: [16, 0] },
  { name: 'LSE',      tz: 'Europe/London',     open: [8, 0],   close: [16, 30] },
  { name: 'Euronext', tz: 'Europe/Paris',      open: [9, 0],   close: [17, 30] },
  { name: 'HKEX',     tz: 'Asia/Hong_Kong',    open: [9, 30],  close: [16, 0] },
  { name: 'TSE',      tz: 'Asia/Tokyo',        open: [9, 0],   close: [15, 0] },
];

export function marketStatuses(now = new Date()) {
  return EXCHANGES.map(({ name, tz, open, close }) => {
    const parts = new Intl.DateTimeFormat('en-US', {
      timeZone: tz, hour: '2-digit', minute: '2-digit', hour12: false, weekday: 'short',
    }).formatToParts(now);
    const hour = Number(parts.find((p) => p.type === 'hour').value);
    const minute = Number(parts.find((p) => p.type === 'minute').value);
    const weekday = parts.find((p) => p.type === 'weekday').value;

    const minutesNow = hour * 60 + minute;
    const openMinutes = open[0] * 60 + open[1];
    const closeMinutes = close[0] * 60 + close[1];
    const isWeekday = weekday !== 'Sat' && weekday !== 'Sun';
    const isOpen = isWeekday && minutesNow >= openMinutes && minutesNow < closeMinutes;

    const localTime = new Intl.DateTimeFormat('en-US', {
      timeZone: tz, hour: 'numeric', minute: '2-digit', hour12: true,
    }).format(now);

    return { name, localTime, isOpen };
  });
}
