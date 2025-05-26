import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom'; 
import PromptForm from './components/PromptForm';
import ImageComposer from './components/ImageComposer';

function App() {
  return (
    <BrowserRouter>  
      <Routes>
        <Route path="/" element={<PromptForm />} />
        <Route path="/compose" element={<ImageComposer />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
