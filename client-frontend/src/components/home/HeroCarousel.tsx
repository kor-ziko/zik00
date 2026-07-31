import ChevronLeft from 'lucide-react/dist/esm/icons/chevron-left.js';
import ChevronRight from 'lucide-react/dist/esm/icons/chevron-right.js';
import { useEffect, useState } from 'react';
import { heroSlides } from '../../data';
import { useLocale } from '../../locale';

const AUTO_PLAY_DELAY = 6000;

function HeroCarousel() {
  const { copy } = useLocale();
  const [activeSlide, setActiveSlide] = useState(0);

  useEffect(() => {
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
    if (reducedMotion.matches || heroSlides.length < 2) return undefined;

    let timer: number | undefined;
    const stopTimer = () => {
      if (timer !== undefined) window.clearInterval(timer);
      timer = undefined;
    };
    const startTimer = () => {
      if (document.hidden || timer !== undefined) return;
      timer = window.setInterval(() => {
        setActiveSlide((current) => (current + 1) % heroSlides.length);
      }, AUTO_PLAY_DELAY);
    };
    const handleVisibilityChange = () => {
      if (document.hidden) stopTimer();
      else startTimer();
    };
    startTimer();
    document.addEventListener('visibilitychange', handleVisibilityChange);

    return () => {
      stopTimer();
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, []);

  const selectedSlide = {
    ...heroSlides[activeSlide],
    ...copy.hero.slides[activeSlide],
  };
  const showPreviousSlide = () => {
    setActiveSlide((current) => (current - 1 + heroSlides.length) % heroSlides.length);
  };
  const showNextSlide = () => {
    setActiveSlide((current) => (current + 1) % heroSlides.length);
  };

  return (
    <section className="hero" aria-label={copy.hero.ariaLabel}>
      <img src={selectedSlide.image} alt="" decoding="async" fetchPriority={activeSlide === 0 ? 'high' : 'auto'} />
      <div className="hero-overlay" />
      <div className="hero-content header-inner">
        <div className="hero-copy">
          <span style={{ color: selectedSlide.accent }}>{selectedSlide.eyebrow}</span>
          <h1>{selectedSlide.title}</h1>
          <p>{selectedSlide.description}</p>
          <a href="#recommendations">{copy.hero.cta} <ChevronRight size={17} /></a>
        </div>
      </div>

      <button className="hero-arrow hero-arrow-left" type="button" onClick={showPreviousSlide} aria-label={copy.hero.previous}>
        <ChevronLeft size={25} />
      </button>
      <button className="hero-arrow hero-arrow-right" type="button" onClick={showNextSlide} aria-label={copy.hero.next}>
        <ChevronRight size={25} />
      </button>

      <div className="hero-dots" aria-label={copy.hero.select}>
        {heroSlides.map((slide, index) => (
          <button
            key={slide.eyebrow}
            className={activeSlide === index ? 'active' : ''}
            type="button"
            onClick={() => setActiveSlide(index)}
            aria-label={`${copy.hero.select} ${index + 1}`}
            aria-current={activeSlide === index}
          />
        ))}
      </div>
    </section>
  );
}

export default HeroCarousel;
