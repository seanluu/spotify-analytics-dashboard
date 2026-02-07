# Spotify Dashboard

Spotify Dashboard is a modern, responsive web application that provides personalized Spotify analytics and insights. Users can connect their Spotify account to view their top artists, tracks, and genres with beautiful visualizations and interactive charts. Discover your music taste patterns across different time periods and explore your listening habits in an elegant, mobile-first interface.

## Installation

**Prerequisites:** [Docker](https://docs.docker.com/get-docker/) and [Docker Compose](https://docs.docker.com/compose/install/) installed

```bash
# Clone the repository
git clone https://github.com/seanluu/spotify-dashboard-new.git
cd spotify-dashboard-new

# Add your Spotify credentials to .env file
# SPOTIFY_CLIENT_ID=your_client_id
# SPOTIFY_CLIENT_SECRET=your_client_secret

# Start everything
docker compose up -d --build
```

Open http://localhost:3000

<details>
<summary><b>Manual Setup (without Docker)</b></summary>

**Prerequisites:** Java 17, Maven, Node.js 20+

### Backend Setup
```bash
cd backend
echo "SPOTIFY_CLIENT_ID=your_client_id_here" > .env
echo "SPOTIFY_CLIENT_SECRET=your_client_secret_here" >> .env
echo "SPOTIFY_REDIRECT_URI=http://localhost:3000/callback" >> .env
./mvnw spring-boot:run -q
```

### Frontend Setup
```bash
cd frontend
npm install
echo "NEXT_PUBLIC_SPOTIFY_CLIENT_ID=your_client_id_here" > .env.local
echo "NEXT_PUBLIC_SPOTIFY_REDIRECT_URI=http://localhost:3000/callback" >> .env.local
echo "NEXT_PUBLIC_API_BASE_URL=http://localhost:8080" >> .env.local
npm run dev
```

</details>

### Set up Spotify App
1. Go to [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
2. Create a new app
3. Note your **Client ID** and **Client Secret**
4. Add `http://localhost:3000/callback` to **Redirect URIs**

## Deployment

Deployed on Vercel: [spotify-dashboard.vercel.app](https://spotify-dashboard.vercel.app)

## Usage

1. Visit the home page and click "Connect with Spotify"
2. Authorize the application with your Spotify account
3. Explore your top artists in a beautiful 3-column grid
4. View your favorite tracks with album artwork
5. Analyze your music taste with interactive genre charts
6. Switch between time ranges (4 weeks, 6 months, all time)
7. View audio feature insights and mood analysis
8. Check the monitoring dashboard for backend health

## Features

### Core Analytics
- **Authentication**: Secure Spotify OAuth 2.0 login with persistent sessions
- **Top Artists**: Beautiful grid layout with artist images and play counts
- **Top Tracks**: Track list with album artwork and preview links
- **Top Genres**: Interactive pie charts showing music taste distribution
- **Time Range Selection**: Analyze habits across different periods (4 weeks, 6 months, all time)
- **Responsive Design**: Mobile-first design that scales to desktop

### Advanced Features (PostgreSQL-backed)

#### Persistent Sessions
- **Encrypted refresh tokens**: Securely store OAuth tokens in database using Jasypt encryption
- **Auto-refresh**: Seamlessly refresh expired access tokens without re-login
- **User profiles**: Store Spotify user data (display name, email, account type, country)

#### Listening History Tracking
- **Automatic tracking**: Scheduled polling of Spotify listening history
- **Data collection**: Stores listening history for audio features analysis
- **Manual updates**: Force an immediate poll via API endpoint

#### Audio Features Analysis (Hybrid ETL Pipeline)
- **Dual data sources**: Listening history + Spotify Top Tracks API (4 weeks / 6 months / all time)
- **Play-count weighted analysis**: Recent period weighted by actual listening frequency
- **Audio metrics**: Energy, valence (mood), danceability, tempo, acousticness for 100+ tracks
- **Mood detection**: Classification based on valence/energy patterns (Happy and Energetic, Sad and Calm, etc.)
- **Trend comparison**: Compare mood/energy across 4 different time periods

#### Playlist Generation
- **Auto-generate playlists**: Create Spotify playlists from your top tracks for any time range

### Monitoring and Observability
- **Prometheus metrics**: Backend instrumented with Spring Boot Actuator + Micrometer, exposing metrics at `/actuator/prometheus`
- **Grafana dashboards**: Pre-provisioned dashboard tracking request rate, p95 latency, JVM memory, and backend uptime
- **Automated alerts**: Alertmanager rules for backend downtime, 5xx error rate > 5%, and p95 latency spikes
- **Infrastructure exporters**: Redis and PostgreSQL metrics scraped via dedicated Prometheus exporters

## Tech Stack

**Frontend**: Next.js 16 (App Router), React, TypeScript, Tailwind CSS, Recharts, Heroicons, Axios
**Backend**: Spring Boot 3.4, Spring Security, Spring Data JPA, Spring Cache, Maven
**Database**: PostgreSQL 16 (with persistent volumes)
**Caching**: Redis 7
**Security**: OAuth 2.0, Jasypt (token encryption)
**Monitoring**: Prometheus, Grafana, Alertmanager, Micrometer
**APIs**: Spotify Web API
**DevOps**: Docker, Docker Compose

## API Endpoints

### User
- `GET /api/v1/spotify/me` - Current user profile

### Top Items
- `GET /api/v1/spotify/top/tracks` - Top tracks (supports `time_range` and `limit` params)
- `GET /api/v1/spotify/top/artists` - Top artists (supports `time_range` and `limit` params)

### Analytics
- `GET /api/v1/spotify/analytics/genres` - Genre distribution
- `GET /api/v1/spotify/audio-features/insights-from-top` - Audio insights from top tracks

### Authentication
- `POST /api/v1/spotify/auth/callback` - Handle OAuth callback and token exchange

### Playlists
- `POST /api/v1/spotify/playlists/generate` - Generate playlist from top tracks

### Listening History
- `POST /api/v1/spotify/listening-history/poll` - Manually poll recent plays from Spotify

### Audio Features
- `POST /api/v1/spotify/audio-features/fetch` - Fetch audio features for top tracks

### Health and Metrics
- `GET /actuator/health` - Backend health check
- `GET /actuator/prometheus` - Prometheus metrics endpoint

## Services (Docker Compose)

| Service | Port | Description |
|---------|------|-------------|
| Frontend | 3000 | Next.js app |
| Backend | 8080 | Spring Boot API |
| PostgreSQL | 5432 | Primary database |
| Redis | 6379 | Caching layer |
| Prometheus | 9090 | Metrics collection |
| Grafana | 3001 | Monitoring dashboards |
| Alertmanager | 9093 | Alert routing |
| Redis Exporter | - | Redis metrics for Prometheus |
| Postgres Exporter | - | PostgreSQL metrics for Prometheus |
