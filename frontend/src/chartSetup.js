// Registers the Chart.js building blocks used across the dashboard charts.
// Imported once from main.jsx so every page can just `import { Line, Doughnut,
// Bar } from 'react-chartjs-2'` without re-registering.
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  Tooltip,
  Legend,
  Filler,
} from 'chart.js';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, ArcElement, Tooltip, Legend, Filler);
