# 🎵 Spotify Dashboard

Spotify Dashboard is a modern, responsive web application that provides personalized Spotify analytics and insights. Users can connect their Spotify account to view their top artists, tracks, and genres with beautiful visualizations and interactive charts. Discover your music taste patterns across different time periods and explore your listening habits in an elegant, mobile-first interface.

## 🔧 Installation

**Prerequisites:** [Docker](https://docs.docker.com/get-docker/) and [Docker Compose](https://docs.docker.com/compose/install/) installed

```bash
# Clone the repository
git clone https://github.com/seanluu/spotify-dashboard-new.git
cd spotify-dashboard-new

# Add your Spotify credentials to .env file
# SPOTIFY_CLIENT_ID=your_client_id
# SPOTIFY_CLIENT_SECRET=your_client_secret

# Start everything
docker-compose up
```

Open http://localhost:3000 ✨

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

## 🌐 Deployment

Deployed on Vercel: [spotify-dashboard.vercel.app](https://spotify-dashboard.vercel.app)

## 🚀 Usage

1. Visit the home page and click "Connect with Spotify"
2. Authorize the application with your Spotify account
3. Explore your top artists in a beautiful 3-column grid
4. View your favorite tracks with album artwork
5. Analyze your music taste with interactive genre charts
6. Switch between time ranges (4 weeks, 6 months, all time)
7. Resize the window to see the responsive layout

## ✨ Features

- **Authentication**: Secure Spotify OAuth login
- **Top Artists**: Beautiful grid layout with artist images and play counts
- **Top Tracks**: Track list with album artwork and preview links
- **Top Genres**: Interactive pie charts showing music taste distribution
- **Time Range Selection**: Analyze habits across different periods
- **Responsive Design**: Mobile-first design that scales to desktop
- **Real-time Data**: Live Spotify API integration

## 🧰 Tech Stack

**Frontend**: Next.js 15 (App Router), React, TypeScript, Tailwind CSS, Recharts, Heroicons
**Backend**: Spring Boot 3.4, Spring Security, Spring Web, Maven
**API**: Spotify Web API
**DevOps**: Docker, Docker Compose
