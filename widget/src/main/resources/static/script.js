// Widget elements
const widget = document.getElementById('widget');
const songNameElem = document.getElementById('song-name');
const artistNameElem = document.getElementById('artist-name');
const leftEye = document.querySelector('.left-eye');
const rightEye = document.querySelector('.right-eye');

// Auto-skip settings
let autoSkipEnabled = false;
let skipInterval = 30000; // Skip every 30 seconds by default
let skipTimer = null;
let currentSongId = null;

// Dragging functionality
let isDragging = false;
let currentX;
let currentY;
let initialX;
let initialY;
let xOffset = 0;
let yOffset = 0;

// Get saved position from localStorage (if available)
function loadPosition() {
    const savedX = localStorage.getItem('widgetX');
    const savedY = localStorage.getItem('widgetY');

    if (savedX && savedY) {
        xOffset = parseInt(savedX);
        yOffset = parseInt(savedY);
        widget.style.transform = `translate(${xOffset}px, ${yOffset}px)`;
    }
}

// Save position to localStorage
function savePosition() {
    localStorage.setItem('widgetX', xOffset);
    localStorage.setItem('widgetY', yOffset);
}

// Drag event handlers
function dragStart(e) {
    if (e.type === "touchstart") {
        initialX = e.touches[0].clientX - xOffset;
        initialY = e.touches[0].clientY - yOffset;
    } else {
        initialX = e.clientX - xOffset;
        initialY = e.clientY - yOffset;
    }

    if (e.target === widget || widget.contains(e.target)) {
        isDragging = true;
        widget.classList.add('dragging');
    }
}

function dragEnd() {
    initialX = currentX;
    initialY = currentY;
    isDragging = false;
    widget.classList.remove('dragging');
    savePosition();
}

function drag(e) {
    if (isDragging) {
        e.preventDefault();

        if (e.type === "touchmove") {
            currentX = e.touches[0].clientX - initialX;
            currentY = e.touches[0].clientY - initialY;
        } else {
            currentX = e.clientX - initialX;
            currentY = e.clientY - initialY;
        }

        xOffset = currentX;
        yOffset = currentY;

        // Keep widget within screen bounds
        const rect = widget.getBoundingClientRect();
        const maxX = window.innerWidth - rect.width;
        const maxY = window.innerHeight - rect.height;

        xOffset = Math.max(0, Math.min(xOffset, maxX));
        yOffset = Math.max(0, Math.min(yOffset, maxY));

        widget.style.transform = `translate(${xOffset}px, ${yOffset}px)`;
    }
}

// Add event listeners for dragging
widget.addEventListener('mousedown', dragStart);
document.addEventListener('mousemove', drag);
document.addEventListener('mouseup', dragEnd);

// Touch events for mobile
widget.addEventListener('touchstart', dragStart);
document.addEventListener('touchmove', drag);
document.addEventListener('touchend', dragEnd);

// Enhanced blinking animation with random intervals
function startBlinking() {
    function scheduleNextBlink() {
        // Random interval between 2-8 seconds (2000-8000ms)
        const randomInterval = Math.floor(Math.random() * 6000) + 2000;

        setTimeout(() => {
            // Trigger blink
            leftEye.classList.add('blinking');
            rightEye.classList.add('blinking');

            // Remove blink after animation
            setTimeout(() => {
                leftEye.classList.remove('blinking');
                rightEye.classList.remove('blinking');
            }, 300);

            // Schedule next blink
            scheduleNextBlink();
        }, randomInterval);
    }

    // Start the first blink cycle
    scheduleNextBlink();
}

// Auto-skip functionality
function skipToNextSong() {
    fetch('http://127.0.0.1:8888/skip-next', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        }
    })
        .then(response => {
            if (response.ok) {
                console.log('Skipped to next song');
                // Update immediately after skipping
                setTimeout(updateSongInfo, 1000);
            }
        })
        .catch(error => {
            console.error('Failed to skip song:', error);
        });
}

function startAutoSkip() {
    if (skipTimer) {
        clearInterval(skipTimer);
    }

    skipTimer = setInterval(() => {
        if (autoSkipEnabled && currentSongId) {
            skipToNextSong();

            // Add visual feedback for skip
            widget.style.transform += ' scale(1.1)';
            setTimeout(() => {
                widget.style.transform = widget.style.transform.replace(' scale(1.1)', '');
            }, 200);
        }
    }, skipInterval);
}

function stopAutoSkip() {
    if (skipTimer) {
        clearInterval(skipTimer);
        skipTimer = null;
    }
}

// Toggle auto-skip on double-click
widget.addEventListener('dblclick', () => {
    autoSkipEnabled = !autoSkipEnabled;

    if (autoSkipEnabled) {
        startAutoSkip();
        showNotification('Auto-skip enabled! 🎵');
    } else {
        stopAutoSkip();
        showNotification('Auto-skip disabled');
    }
});

// Show temporary notification
function showNotification(message) {
    const notification = document.createElement('div');
    notification.style.cssText = `
    position: fixed;
    top: 20px;
    right: 20px;
    background: rgba(0,0,0,0.8);
    color: white;
    padding: 10px 20px;
    border-radius: 10px;
    font-size: 12px;
    z-index: 1001;
    animation: slideIn 0.3s ease;
  `;
    notification.textContent = message;

    document.body.appendChild(notification);

    setTimeout(() => {
        notification.remove();
    }, 2000);
}

// Update widget color and state
function updateWidgetState(isPlaying, hasSong) {
    widget.classList.remove('playing', 'paused', 'no-song');

    if (!hasSong) {
        widget.classList.add('no-song');
    } else if (isPlaying) {
        widget.classList.add('playing');
    } else {
        widget.classList.add('paused');
    }
}

// Handle long text scrolling
function handleLongText(element, text, maxLength = 25) {
    element.textContent = text;

    if (text.length > maxLength) {
        element.classList.add('scroll-text');
    } else {
        element.classList.remove('scroll-text');
    }
}

// Eye dancing animation when music is playing
function toggleEyeDancing(isPlaying) {
    if (isPlaying) {
        leftEye.classList.add('dancing');
        rightEye.classList.add('dancing');
    } else {
        leftEye.classList.remove('dancing');
        rightEye.classList.remove('dancing');
    }
}

// Main function to update song info
async function updateSongInfo() {
    try {
        const response = await fetch('http://127.0.0.1:8888/current-song');

        if (!response.ok) {
            if (response.status === 401) {
                showLoginPrompt();
                return;
            }
            throw new Error('Network response was not ok');
        }

        const data = await response.json();

        if (data.message === "No song currently playing") {
            handleLongText(songNameElem, 'No song playing');
            artistNameElem.textContent = '';
            updateWidgetState(false, false);
            toggleEyeDancing(false);
            currentSongId = null;
            return;
        }

        if (data.item) {
            const songName = data.item.name || 'Unknown Song';
            const artistName = data.item.artists?.map(artist => artist.name).join(', ') || 'Unknown Artist';
            const isPlaying = data.is_playing || false;
            const newSongId = data.item.id;

            // Check if song changed
            if (currentSongId !== newSongId) {
                currentSongId = newSongId;

                // Visual feedback for new song
                widget.style.background = 'linear-gradient(135deg, #ff6b6b, #feca57)';
                setTimeout(() => {
                    updateWidgetState(isPlaying, true);
                }, 500);
            }

            handleLongText(songNameElem, songName);
            handleLongText(artistNameElem, artistName);

            updateWidgetState(isPlaying, true);
            toggleEyeDancing(isPlaying);

            // Hide login prompt if it was shown
            hideLoginPrompt();
        }

    } catch (error) {
        console.error('Failed to fetch current song:', error);
        handleLongText(songNameElem, 'Connection error');
        artistNameElem.textContent = 'Check your connection';
        updateWidgetState(false, false);
        toggleEyeDancing(false);
        currentSongId = null;
    }
}

// Show login prompt
function showLoginPrompt() {
    songNameElem.innerHTML = '<div class="login-prompt">Please <a href="http://127.0.0.1:8888/login" target="_blank" class="login-link">login to Spotify</a></div>';
    artistNameElem.textContent = '';
    updateWidgetState(false, false);
    toggleEyeDancing(false);
}

// Hide login prompt
function hideLoginPrompt() {
    if (songNameElem.innerHTML.includes('login-prompt')) {
        songNameElem.textContent = 'Connecting...';
    }
}

// Initialize widget
function initializeWidget() {
    loadPosition();
    startBlinking();
    updateSongInfo();

    // Update every 3 seconds for more responsive updates
    setInterval(updateSongInfo, 3000);

    // Show instructions
    setTimeout(() => {
        showNotification('Double-click to toggle auto-skip! 🎵');
    }, 3000);
}

// Handle window resize to keep widget in bounds
window.addEventListener('resize', () => {
    const rect = widget.getBoundingClientRect();
    const maxX = window.innerWidth - rect.width;
    const maxY = window.innerHeight - rect.height;

    xOffset = Math.max(0, Math.min(xOffset, maxX));
    yOffset = Math.max(0, Math.min(yOffset, maxY));

    widget.style.transform = `translate(${xOffset}px, ${yOffset}px)`;
    savePosition();
});

// Prevent text selection during drag
widget.addEventListener('selectstart', (e) => {
    e.preventDefault();
});

// Start the widget
window.addEventListener('DOMContentLoaded', initializeWidget);